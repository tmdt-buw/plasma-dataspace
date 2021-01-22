import {EntityType} from '../../../api/generated/dms';

export class EntityDroppedEvent {
  primitiveNodeUUID?: string;
  entityType: EntityType;

  constructor(entityType: EntityType, primitiveNodeUUID?: string) {
    this.primitiveNodeUUID = primitiveNodeUUID;
    this.entityType = entityType;
  }
}
