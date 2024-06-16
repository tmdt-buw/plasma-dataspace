export * from './analysisController.service';
import { AnalysisControllerService } from './analysisController.service';
export * from './modelingController.service';
import { ModelingControllerService } from './modelingController.service';
export const APIS = [AnalysisControllerService, ModelingControllerService];
