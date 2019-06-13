/*
 * Copyright (c) 2017-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.targetedms.parser.skyd;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;

/**
 * Created by nicksh on 2/27/2017.
 */
public abstract class StructSerializer<TItem>
{
    private final Class<TItem> itemClass;
    private int itemSizeInMemory;
    private int itemSizeOnDisk;

    public StructSerializer(Class<TItem> itemClass, int itemSizeInMemory, int itemSizeOnDisk) {
        this.itemClass = itemClass;
        this.itemSizeInMemory = itemSizeInMemory;
        this.itemSizeOnDisk = itemSizeOnDisk;
    }

    public void readArray(InputStream stream, TItem[] array) throws IOException {
        byte[] buffer = new byte[getItemSizeOnDisk()];
        for (int i = 0; i < array.length; i++) {
            if (stream.read(buffer) != buffer.length) {
                throw new IOException();
            }
            array[i] = fromByteArray(resizeByteArray(buffer, getItemSizeInMemory()));
        }
    }

    @SuppressWarnings("unchecked")
    public TItem[] readArray(InputStream inputStream, int count) throws IOException {
        TItem[] array = (TItem[]) Array.newInstance(itemClass, count);
        readArray(inputStream, array);
        return array;
    }

    public byte[] resizeByteArray(byte[] byteArray, int newSize) {
        if (byteArray.length == newSize) {
            return byteArray;
        }
        byte[] newByteArray = new byte[newSize];
        if (isPadFromStart()) {
            System.arraycopy(byteArray, Math.max(0, byteArray.length - newSize),
                    newByteArray, Math.max(0, newSize - byteArray.length), Math.min(byteArray.length, newSize));
        } else {
            System.arraycopy(byteArray, 0,
                    newByteArray, 0, Math.min(byteArray.length, newSize));
        }
        return newByteArray;
    }

    public abstract TItem fromByteArray(byte[] bytes);

    public int getItemSizeInMemory()
    {
        return itemSizeInMemory;
    }

    public void setItemSizeInMemory(int itemSizeInMemory)
    {
        this.itemSizeInMemory = itemSizeInMemory;
    }

    public int getItemSizeOnDisk()
    {
        return itemSizeOnDisk;
    }

    public void setItemSizeOnDisk(int itemSizeOnDisk)
    {
        this.itemSizeOnDisk = itemSizeOnDisk;
    }

    protected boolean isPadFromStart()
    {
        return false;
    }
}
