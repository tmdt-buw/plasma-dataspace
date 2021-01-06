import { EntityTypeDTO } from '../../../api/generated/dms';
import { SyntaxNode } from '../node/syntax-node';
import { Node } from '../node/node';

export class NodeChangedEvent {
  type: Node;
  node: SyntaxNode | EntityTypeDTO;

  constructor(type: Node, node: SyntaxNode | EntityTypeDTO) {
    this.type = type;
    this.node = node;
  }
}
