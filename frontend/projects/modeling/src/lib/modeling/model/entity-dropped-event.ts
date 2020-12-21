import { EntityConceptDTO } from '../../api/generated/dms';

export class EntityDroppedEvent {
  primitiveNodeUUID?: string;
  entityConceptDTO: EntityConceptDTO

  constructor(entityConceptDTO: EntityConceptDTO, primitiveNodeUUID?: string) {
    this.primitiveNodeUUID = primitiveNodeUUID;
    this.entityConceptDTO = entityConceptDTO;
  }
}
