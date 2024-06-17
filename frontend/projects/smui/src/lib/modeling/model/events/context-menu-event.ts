import { Relation, SemanticNode } from '../../../api/generated/sms';

export class ContextMenuEvent {
  type: ContextMenuEventType;
  target: SemanticNode | Relation;

  constructor(type: ContextMenuEventType, template: SemanticNode | Relation) {
    this.type = type;
    this.target = template;
  }
}

export enum ContextMenuEventType {
  removeRelation, removeEntity, performOperation, updateEntity, updateRelation
}
