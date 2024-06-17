import { SemanticNode, SyntaxNode } from '../../../api/generated/sms';
import { NodeType } from '../configuration/node';

export class NodeChangedEvent {
  type: NodeType;
  node: SyntaxNode | SemanticNode;

  constructor(type: NodeType, node: SyntaxNode | SemanticNode) {
    this.type = type;
    this.node = node;
  }
}
