export type Node = 'custom.SyntaxNode' | 'custom.SemanticNode';

export abstract class Nodes {
  static readonly syntaxNodeName: Node = 'custom.SyntaxNode';
  static readonly semanticNodeName: Node = 'custom.SemanticNode';
}
