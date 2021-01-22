import { EntityConcept, RelationConcept, SyntacticOperationDTO } from '../../../api/generated/dms';

export class ContextMenuEvent {
  type: ContextMenuEventType;
  target: EntityConcept | RelationConcept;
  operation?: SyntacticOperationDTO;

  constructor(type: ContextMenuEventType, concept: EntityConcept | RelationConcept, operation?: SyntacticOperationDTO) {
    this.type = type;
    this.target = concept;
    this.operation = operation;
  }
}

export enum ContextMenuEventType {
  removeRelation, removeEntity, performOperation
}
