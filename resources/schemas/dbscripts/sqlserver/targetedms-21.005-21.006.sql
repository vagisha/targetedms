-- FileNameHint for a Skyline document with a "document" library will be one character more than the name of the .sky file
-- Look at PeptideSettingsParser.getDocumentLibrary()
-- Example:      Stergachis-SupplementaryData_2_a.sky
-- FileNameHint: Stergachis-SupplementaryData_2_a.blib
-- Current limit on the targetedms.runs FileName column that stores the name of the .sky.zip file (e.g. Stergachis-SupplementaryData_2_a.sky.zip)
-- is 300. Set FileNameHint to be the same.
ALTER TABLE targetedms.spectrumlibrary ALTER COLUMN FileNameHint NVARCHAR(300);

-- Users can enter a comma-separated list of amino acids for both structural and isotopic modifications in Skyline.
-- If they enter all 20 standard amino acids plus O (Pyrrolysine) and U (Selenocysteine) the length of the string
-- we need to store in the AminoAcid column will be 64: A, C, D, E, F, G, H, I, K, L, M, N, O, P, Q, R, S, T, U, V, W, Y
ALTER TABLE targetedms.IsotopeModification ALTER COLUMN AminoAcid NVARCHAR(100);
ALTER TABLE targetedms.StructuralModification ALTER COLUMN AminoAcid NVARCHAR(100);