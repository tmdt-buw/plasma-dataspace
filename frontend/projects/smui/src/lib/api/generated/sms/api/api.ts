export * from './arsRegistrationController.service';
import { ArsRegistrationControllerService } from './arsRegistrationController.service';
export * from './modelingController.service';
import { ModelingControllerService } from './modelingController.service';
export * from './schemaAnalysisController.service';
import { SchemaAnalysisControllerService } from './schemaAnalysisController.service';
export const APIS = [ArsRegistrationControllerService, ModelingControllerService, SchemaAnalysisControllerService];
