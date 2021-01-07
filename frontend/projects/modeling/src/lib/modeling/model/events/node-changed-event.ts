import { EntityTypeDTO } from '../../../api/generated/dms';
import { SyntaxNode } from '../configuration/node';
import { Node } from '../configuration/node';

export class NodeChangedEvent {
  type: Node;
  node: SyntaxNode | EntityTypeDTO;

  constructor(type: Node, node: SyntaxNode | EntityTypeDTO) {
    this.type = type;
    this.node = node;
  }
}
