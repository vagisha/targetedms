UPDATE targetedms.transitionpredictionsettings SET CePredictorId = NULL WHERE CePredictorId NOT IN (SELECT Id FROM targetedms.Predictor);
UPDATE targetedms.transitionpredictionsettings SET DpPredictorId = NULL WHERE DpPredictorId NOT IN (SELECT Id FROM targetedms.Predictor);

ALTER TABLE targetedms.transitionpredictionsettings ADD CONSTRAINT FK_TransitionPredictionSettings_PredictorCe FOREIGN KEY (CePredictorId) REFERENCES targetedms.Predictor(Id);
ALTER TABLE targetedms.transitionpredictionsettings ADD CONSTRAINT FK_TransitionPredictionSettings_PredictorDp FOREIGN KEY (DpPredictorId) REFERENCES targetedms.Predictor(Id);

CREATE INDEX IX_TransitionPredictionSettings_CePredictorId ON targetedms.transitionpredictionsettings(CePredictorId);
CREATE INDEX IX_TransitionPredictionSettings_DpPredictorId ON targetedms.transitionpredictionsettings(DpPredictorId);

DROP INDEX targetedms.TransitionOptimization.IX_TransitionOptimization_TransitionId;
GO
ALTER TABLE targetedms.TransitionOptimization DROP CONSTRAINT FK_TransitionOptimization_TransitionId;
GO
ALTER TABLE targetedms.TransitionOptimization ADD CONSTRAINT FK_TransitionOptimization_TransitionId FOREIGN KEY (TransitionId) REFERENCES targetedms.GeneralTransition(Id);
GO
CREATE INDEX IX_TransitionOptimization_TransitionId ON targetedms.TransitionOptimization (TransitionId);
GO