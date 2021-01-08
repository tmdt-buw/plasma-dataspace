import {EntityType} from '../../../api/generated/dms';
import {Node, SyntaxNode} from '../configuration/node';

export class NodeChangedEvent {
  type: Node;
  node: SyntaxNode | EntityType;

  constructor(type: Node, node: SyntaxNode | EntityType) {
    this.type = type;
    this.node = node;
  }
}
