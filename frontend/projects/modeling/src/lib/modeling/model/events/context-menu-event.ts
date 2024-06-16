import { Relation, SemanticModelNode } from '../../../api/generated/sms';

export class ContextMenuEvent {
  type: ContextMenuEventType;
  target: SemanticModelNode | Relation;

  constructor(type: ContextMenuEventType, template: SemanticModelNode | Relation) {
    this.type = type;
    this.target = template;
  }
}

export enum ContextMenuEventType {
  removeRelation, removeEntity, performOperation, updateEntity, updateRelation
}
