export * from './localKnowledgeController.service';
import {LocalKnowledgeControllerService} from './localKnowledgeController.service';
import {QueryControllerService} from './queryController.service';
import {UniversalKnowledgeGraphControllerService} from './universalKnowledgeGraphController.service';

export * from './queryController.service';
export * from './universalKnowledgeGraphController.service';
export const APIS = [LocalKnowledgeControllerService, QueryControllerService, UniversalKnowledgeGraphControllerService];
