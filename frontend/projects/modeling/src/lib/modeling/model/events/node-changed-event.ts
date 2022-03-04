import { SemanticModelNode } from '../../../api/generated/dms';
import { Node, SyntaxNode } from '../configuration/node';

export class NodeChangedEvent {
  type: Node;
  node: SyntaxNode | SemanticModelNode;

  constructor(type: Node, node: SyntaxNode | SemanticModelNode) {
    this.type = type;
    this.node = node;
  }
}
