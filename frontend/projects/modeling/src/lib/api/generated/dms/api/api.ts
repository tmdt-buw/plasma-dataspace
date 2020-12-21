import { LocalKnowledgeControllerService } from './kgsModelController.service';
import { DataModelingControllerService } from './dataModelingController.service';
import { UniversalKnowledgeGraphControllerService } from './kgsController.service';

export * from './dataModelingController.service';
export const APIS = [DataModelingControllerService, UniversalKnowledgeGraphControllerService, LocalKnowledgeControllerService];
