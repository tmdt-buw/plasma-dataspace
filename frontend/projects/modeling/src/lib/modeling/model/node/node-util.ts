import { dia } from 'jointjs';
import { Nodes } from './node';

export class NodeUtil {

  static isSyntaxNode(node: dia.CellView | dia.ElementView | dia.Element): boolean {
    const model = node instanceof dia.Element ? node : node.model;
    return model.attributes?.type === Nodes.syntaxNodeName;
  }

  static isSemanticNode(node: dia.CellView | dia.ElementView | dia.Element): boolean {
    const model = node instanceof dia.Element ? node : node.model;
    return model.attributes?.type === Nodes.semanticNodeName;
  }

}
