export * from './modelingController.service';
import { ModelingControllerService } from './modelingController.service';

export * from './schemaAnalysisController.service';
import { SchemaAnalysisControllerService } from './schemaAnalysisController.service';

export const APIS = [ModelingControllerService, SchemaAnalysisControllerService];
