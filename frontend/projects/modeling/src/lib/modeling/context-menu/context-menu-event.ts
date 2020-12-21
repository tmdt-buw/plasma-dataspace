import { EntityConceptDTO, RelationConceptDTO } from '../../api/generated/dms';

export class ContextMenuEvent {
  type: ContextMenuEventType;
  concept: EntityConceptDTO | RelationConceptDTO;

  constructor(type: ContextMenuEventType, concept: EntityConceptDTO | RelationConceptDTO) {
    this.type = type;
    this.concept = concept;
  }
}

export enum ContextMenuEventType {
  removeRelation, removeEntity
}
