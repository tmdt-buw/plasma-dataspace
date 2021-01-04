import {
  CollisionSchemaDTO,
  CompositeDTO,
  EntityTypeDTO,
  ObjectNodeDTO,
  PrimitiveDTO,
  SchemaNodeDTO,
  SetNodeDTO
} from '../../../api/generated/dms';

export class NodeChangedEvent {
  type: 'syntax' | 'semantic';
  node: SchemaNodeDTO | CollisionSchemaDTO | CompositeDTO | ObjectNodeDTO | PrimitiveDTO | SetNodeDTO | EntityTypeDTO;

  constructor(type: "syntax" | "semantic", node: SchemaNodeDTO | CollisionSchemaDTO | CompositeDTO | ObjectNodeDTO | PrimitiveDTO | SetNodeDTO | EntityTypeDTO) {
    this.type = type;
    this.node = node;
  }
}
