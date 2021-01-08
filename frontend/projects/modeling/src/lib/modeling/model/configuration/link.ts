import { dia, g, shapes } from 'jointjs';
import { Nodes } from './node';
import { groupBy } from 'lodash';

/**
 * Link type def
 */
export type Link = 'custom.SyntaxLink' | 'custom.SemanticLink';

/**
 * Abstract class that provides configs for links
 */
export abstract class Links {

  // internal link type names
  static readonly syntaxLinkName: Link = 'custom.SyntaxLink';
  static readonly semanticLinkName: Link = 'custom.SemanticLink';

  // basic semantic link config
  private static semanticLink = shapes.standard.Link.define(
    Links.semanticLinkName, {
      attrs: {
        line: {
          strokeWidth: 1
        }
      }
    }
  );

  // basic syntax link config
  private static syntaxLink = shapes.standard.Link.define(
    Links.syntaxLinkName, {
      attrs: {
        line: {
          strokeOpacity: 0.75,
          strokeWidth: 1,
          strokeDasharray: '10,2'
        }
      }
    }
  );

  /**
   * Creates label
   * @param label text
   * @param color of text
   * @param opacity of text
   */
  private static createLabel(label: string, color: string, opacity?: number): dia.Link.Label[] {
    return label ? [{
      attrs: {
        text: {
          text: label,
          fill: color,
          opacity: opacity ? opacity : 1
        }
      },
      position: {
        distance: 0.5,
        offset: (label.indexOf('\n') > -1 || label.length === 1) ? 0 : 10,
        args: {
          keepGradient: true,
          ensureLegibility: true
        }
      }
    }] : undefined;
  }

  /**
   * Returns true if input link is a syntax link
   * @param link to test
   */
  static isSyntaxLink(link: dia.CellView | dia.LinkView): boolean {
    return link.model?.attributes?.type === Links.syntaxLinkName;
  }

  /**
   * Returns true if input link is a semantic link
   * @param link to test
   */
  static isSemanticLink(link: dia.CellView | dia.LinkView): boolean {
    return link.model?.attributes?.type === Links.semanticLinkName;
  }

  /**
   * Returns true if input link targets a semantic node
   * @param link to test
   */
  static targetsSemanticNode(link: dia.LinkView): boolean {
    return link.model?.getTargetElement()?.attributes?.type === Nodes.semanticNodeName;
  }

  /**
   * Calls constructor and return new default link
   * @param label of link
   * @param color of link
   */
  static createDefaultLink(label: string, color: string): dia.Link {
    const link = new Links.semanticLink({
      // @ts-ignore not in jointjs model but works this way
      labels: Links.createLabel(label, color)
    });
    // link style
    link.router('normal');
    link.connector('jumpover');
    return link;
  }

  /**
   * Calls constructor and return new semantic link
   * @param id of link
   * @param source of link
   * @param target of link
   * @param label of link
   * @param color of link
   * @param opacity of link
   */
  static createSemanticLink(id: string, source: string, target: string, label: string, color: string, opacity?: number): dia.Link {
    const link = new Links.semanticLink({
      id,
      // @ts-ignore not in jointjs model but works this way
      source: {id: source},
      target: {id: target},
      labels: Links.createLabel(label, color, opacity),
      attrs: {
        line: {
          stroke: color,
          opacity: opacity ? opacity : 1
        }
      }
    });
    // link style
    link.router('normal');
    link.connector('jumpover');
    return link;
  }

  /**
   * Call constructor and return new syntax link
   * @param source of link
   * @param target of link
   * @param color of link
   */
  static createSyntaxLink(source: string, target: string, color: string): dia.Link {
    const link = new Links.syntaxLink({
      // @ts-ignore not in jointjs model but works this way
      source: {id: source},
      target: {id: target},
      attrs: {
        line: {
          stroke: color
        }
      }
    });
    // link style
    link.router('normal');
    link.connector('jumpover');
    return link;
  }

  /**
   * Adjust vertices of a node for nice layout of multiple relations between nodes and self links
   * @param graph containing nodes
   * @param cell of node
   */
  static adjustVertices(graph, cell): void {
    // if `cell` is a view, find its model
    cell = cell.model || cell;
    if (cell instanceof dia.Element) {
      // `cell` is an element
      const groups = groupBy(graph.getConnectedLinks(cell), (link) => {
        return [link.source().id, link.target().id].sort();
      });
      Object.entries(groups).forEach(([key, group]) => {
        // adjust vertices for each group
        if (key !== 'undefined') {
          this.adjustVertices(graph, group[0]);
        }
      });
      return;
    }
    // `cell` is a link
    // get its source and target model IDs
    const sourceId = cell.get('source').id || cell.previous('source').id;
    const targetId = cell.get('target').id || cell.previous('target').id;
    // if one of the ends is not a model
    // (if the link is pinned to paper at a point)
    // the link is interpreted as having no siblings
    if (!sourceId || !targetId) {
      return;
    }
    // identify link siblings
    const siblings = graph.getLinks().filter((sibling) => {
      const siblingSourceId = sibling.source().id;
      const siblingTargetId = sibling.target().id;
      // if source and target are the same
      // or if source and target are reversed
      return ((siblingSourceId === sourceId) && (siblingTargetId === targetId))
        || ((siblingSourceId === targetId) && (siblingTargetId === sourceId));
    });
    const numSiblings = siblings.length;
    switch (numSiblings) {
      case 0: {
        // there is no link - should never happen
        break;
      }
      case 1: {
        // there is only one link
        // no vertices needed
        if (sourceId === targetId) {
          Links.layoutSelfLink(graph, cell, sourceId, 0);
        } else {
          cell.unset('vertices');
        }
        break;
      }
      default: {
        // there are multiple siblings
        // we need to create vertices
        if (sourceId === targetId) {
          siblings.forEach((link, index) => Links.layoutSelfLink(graph, link, sourceId, index));
        } else {
          // find the middle point of the link
          const sourceCenter = graph.getCell(sourceId).getBBox().center();
          const targetCenter = graph.getCell(targetId).getBBox().center();
          const midPoint = new g.Line(sourceCenter, targetCenter).midpoint();
          // find the angle of the link
          const theta = sourceCenter.theta(targetCenter);
          // constant
          // the maximum distance between two sibling links
          const GAP = 50;
          siblings.forEach((sibling, index) => {
            // we want offset values to be calculated as 0, 20, 20, 40, 40, 60, 60 ...
            let offset = GAP * Math.ceil(index / 2);
            // place the vertices at points which are `offset` pixels perpendicularly away
            // from the first link
            //
            // as index goes up, alternate left and right
            //
            //  ^  odd indices
            //  |
            //  |---->  index 0 sibling - centerline (between source and target centers)
            //  |
            //  v  even indices
            const sign = ((index % 2) ? 1 : -1);
            // to assure symmetry, if there is an even number of siblings
            // shift all vertices leftward perpendicularly away from the centerline
            if ((numSiblings % 2) === 0) {
              offset -= ((GAP / 2) * sign);
            }
            // make reverse links count the same as non-reverse
            const reverse = ((theta < 180) ? 1 : -1);
            // we found the vertex
            const angle = g.toRad(theta + (sign * reverse * 90));
            const vertex = g.Point.fromPolar(offset, angle, midPoint);
            // replace vertices array with `vertex`
            sibling.vertices([vertex]);
            sibling.connector('smooth');
          });
        }
      }
    }
  }

  /**
   * Calculate layout for self-links
   * @param graph of link
   * @param cell of link
   * @param sourceId of link
   * @param index of link
   */
  private static layoutSelfLink(graph, cell, sourceId: number, index: number): void {
    const source = graph.getCell(sourceId);
    const sourceCenter = source.getBBox().center();
    // find links that are not self-links
    const linkedNodes = graph.getConnectedLinks(source).filter(link => link.source().id !== sourceId || link.target().id !== sourceId);
    // calc center point of those links
    const linksCenters = linkedNodes.map(link => link.getBBox().center());
    const numberLinksCenters = linksCenters.length;
    const centerX = linksCenters.reduce((sum, linkCenter) => sum + linkCenter.x, 0) / numberLinksCenters;
    const centerY = linksCenters.reduce((sum, linkCenter) => sum + linkCenter.y, 0) / numberLinksCenters;
    // find angle between semantic node center and connected links center
    const theta = sourceCenter.theta({x: centerX, y: centerY});
    // two vertex points for nice curve at 170° and 190° (opposite side of links center)
    const vertex1 = g.Point.fromPolar(100 + 50 * index, g.toRad(theta + 170), sourceCenter);
    const vertex2 = g.Point.fromPolar(100 + 50 * index, g.toRad(theta + 190), sourceCenter);
    cell.vertices([vertex1, vertex2]);
    cell.connector('smooth');
  }

}
