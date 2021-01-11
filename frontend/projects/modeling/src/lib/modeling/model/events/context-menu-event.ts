import { EntityConceptDTO, RelationConceptDTO, SyntacticOperationDTO } from '../../../api/generated/dms';

export class ContextMenuEvent {
  type: ContextMenuEventType;
  target: EntityConceptDTO | RelationConceptDTO;
  operation?: SyntacticOperationDTO;

  constructor(type: ContextMenuEventType, concept: EntityConceptDTO | RelationConceptDTO, operation?: SyntacticOperationDTO) {
    this.type = type;
    this.target = concept;
    this.operation = operation;
  }
}

export enum ContextMenuEventType {
  removeRelation, removeEntity, performOperation
}
