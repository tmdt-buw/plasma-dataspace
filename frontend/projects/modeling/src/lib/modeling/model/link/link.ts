export type Link = 'custom.SyntaxLink' | 'custom.SemanticLink';

export abstract class Links {
  static readonly syntaxLinkName: Link = 'custom.SyntaxLink';
  static readonly semanticLinkName: Link = 'custom.SemanticLink';
}
