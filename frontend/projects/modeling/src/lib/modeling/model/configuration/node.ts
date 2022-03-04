import { dia, shapes } from 'jointjs';
import {
  CollisionSchema,
  CompositeNode,
  ObjectNode,
  PrimitiveNode,
  SchemaNode,
  SemanticModelNode,
  SetNode
} from '../../../api/generated/dms';


/**
 * Node type defs
 */
export type Node = 'custom.SyntaxNode' | 'custom.SemanticClassNode';
export type SyntaxNode = SchemaNode | CollisionSchema | CompositeNode | ObjectNode | PrimitiveNode | SetNode;
export type SemanticNode = SemanticModelNode;

/**
 * Abstract class that provides configs for nodes
 */
export abstract class Nodes {

  // internal node type names
  static readonly syntaxNodeName: Node = 'custom.SyntaxNode';
  static readonly semanticClassName: Node = 'custom.SemanticClassNode';

  // basic semantic node config
  private static classNode: dia.Cell.Constructor<dia.Element> = shapes.standard.HeaderedRectangle.define(
    Nodes.semanticClassName, {
      attrs: {
        body: {
          rx: 5,
          ry: 5,
          strokeWidth: 2,
          magnet: true
        },
        header: {
          rx: 5,
          ry: 5,
          pointerEvents: 'none'
        },
        headerText: {
          textVerticalAnchor: 'middle',
          textAnchor: 'middle',
          refX: '50%',
          refY: 15,
          fontSize: 14,
          fill: '#333333',
          'font-family': 'Roboto',
          cursor: 'default',
          pointerEvents: 'none'
        },
        bodyIcon: {
          textVerticalAnchor: 'middle',
          textAnchor: 'left',
          refX: '5',
          refY: '58%',
          refY2: 15,
          fontSize: 24,
          fill: '#333333',
          'font-family': 'Material Icons',
          cursor: 'default',
          pointerEvents: 'none'
        },
        bodyText: {
          textVerticalAnchor: 'middle',
          textAnchor: 'left',
          refX: '35',
          refY: '50%',
          refY2: 15,
          fontSize: 14,
          fill: '#333333',
          'font-family': 'Roboto',
          cursor: 'default',
          pointerEvents: 'none'
        }
      }
    }, {
      markup: [{
        tagName: 'rect',
        selector: 'body'
      }, {
        tagName: 'rect',
        selector: 'header'
      }, {
        tagName: 'text',
        selector: 'headerText'
      }, {
        tagName: 'text',
        selector: 'bodyIcon'
      }, {
        tagName: 'text',
        selector: 'bodyText'
      }]
    }
  );

  // basic syntax node config
  private static syntaxNode: dia.Cell.Constructor<dia.Element> = shapes.standard.Rectangle.define(
    Nodes.syntaxNodeName, {
      attrs: {
        body: {
          rx: 5,
          ry: 5,
          strokeWidth: 2,
          strokeOpacity: 0.75,
          strokeDasharray: '10,2'
        },
        icon: {
          textVerticalAnchor: 'middle',
          textAnchor: 'left',
          refX: '5',
          refY: '65%',
          fontSize: 24,
          fill: '#333333',
          'font-family': 'Material Icons',
          cursor: 'default',
          pointerEvents: 'none'
        },
        label: {
          textVerticalAnchor: 'middle',
          textAnchor: 'left',
          refX: '35',
          refY: '50%',
          fontSize: 14,
          fill: '#333333',
          'font-family': 'Roboto',
          cursor: 'default',
          pointerEvents: 'none'
        }
      }
    }, {
      markup: [{
        tagName: 'rect',
        selector: 'body'
      }, {
        tagName: 'text',
        selector: 'icon'
      }, {
        tagName: 'text',
        selector: 'label'
      }]
    }
  );

  /**
   * Returns true if input node is a syntax node
   * @param node to test
   */
  static isSyntaxNode(node: dia.CellView | dia.ElementView | dia.Element): boolean {
    const model = node instanceof dia.Element ? node : node.model;
    return model.attributes?.type === Nodes.syntaxNodeName;
  }

  /**
   * Returns true if input node is a semantic node
   * @param node to test
   */
  static isSemanticNode(node: dia.CellView | dia.ElementView | dia.Element): boolean {
    const model = node instanceof dia.Element ? node : node.model;
    return model.attributes?.type === Nodes.semanticClassName;
  }

  /**
   * Calls constructor and returns instance of a new semantic class node
   * @param id of node
   * @param header of node
   * @param label of node
   * @param icon of node
   * @param position of node
   * @param width of node
   * @param height of node
   * @param color of node
   * @param opacity of node
   */
  static createSemanticNode(id: string, header: string, label: string, icon: string, position: { x: number, y: number }, width: number, height: number, color: { color: string, contrast: string }, opacity?: number): dia.Element {
    return new Nodes.classNode({
      id,
      // @ts-ignore not in jointjs model but works this way
      position,
      size: {
        width,
        height
      },
      attrs: {
        body: {
          stroke: color.color,
          opacity: opacity ? opacity : 1
        },
        header: {
          fill: color.color,
          stroke: color.color,
          opacity: opacity ? opacity : 1
        },
        headerText: {
          text: header,
          fill: color.contrast,
          opacity: opacity ? opacity : 1
        },
        bodyIcon: {
          text: icon,
          fill: color.color,
          opacity: opacity ? opacity : 1
        },
        bodyText: {
          // adjust text for icon
          refX: icon ? '35' : '50%',
          textAnchor: icon ? 'left' : 'middle',
          text: label,
          fill: color.color,
          opacity: opacity ? opacity : 1
        }
      }
    });
  }

  /**
   * Calls constructor and returns instance of a new syntax node
   * @param id of node
   * @param label of node
   * @param icon of node
   * @param position of node
   * @param width of node
   * @param height of node
   * @param color of node
   */
  static createSyntaxNode(id: string, label: string, icon: string, position: { x: number, y: number }, width: number, height: number, color: string): dia.Element {
    return new Nodes.syntaxNode({
      id,
      // @ts-ignore not in jointjs model but works this way
      position,
      size: {
        width,
        height
      },
      attrs: {
        body: {
          stroke: color
        },
        icon: {
          text: icon,
          fill: color
        },
        label: {
          text: label,
          fill: color
        }
      }
    });
  }
}
