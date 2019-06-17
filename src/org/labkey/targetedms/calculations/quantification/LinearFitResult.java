/*
 * Copyright (c) 2017-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.targetedms.calculations.quantification;

public class LinearFitResult {
    private double estimatedValue;
    private double standardError;
    private double tValue;
    private int degreesOfFreedom;
    private double pValue;

    public LinearFitResult(double estimateValue) {
        this.estimatedValue = estimateValue;
    }

    public double getEstimatedValue() {
        return estimatedValue;
    }

    public void setEstimatedValue(double estimatedValue) {
        this.estimatedValue = estimatedValue;
    }

    public double getStandardError() {
        return standardError;
    }

    public void setStandardError(double standardError) {
        this.standardError = standardError;
    }

    public double getTValue() {
        return tValue;
    }

    public void setTValue(double tValue) {
        this.tValue = tValue;
    }

    public int getDegreesOfFreedom() {
        return degreesOfFreedom;
    }

    public void setDegreesOfFreedom(int degreesOfFreedom) {
        this.degreesOfFreedom = degreesOfFreedom;
    }

    public double getPValue() {
        return pValue;
    }

    public void setPValue(double pValue) {
        this.pValue = pValue;
    }

    @Override
    public String toString() {
        return "LinearFitResult{" +
                "estimatedValue=" + estimatedValue +
                ", standardError=" + standardError +
                ", tValue=" + tValue +
                ", degreesOfFreedom=" + degreesOfFreedom +
                ", pValue=" + pValue +
                '}';
    }
}
