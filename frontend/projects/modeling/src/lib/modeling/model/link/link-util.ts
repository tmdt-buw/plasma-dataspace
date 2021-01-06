import { dia, g } from 'jointjs';
import { groupBy } from 'lodash';
import { Links } from './link';
import { Nodes } from '../node/node';

export class LinkUtil {

  static isSyntaxLink(link: dia.CellView | dia.LinkView): boolean {
    return link.model?.attributes?.type === Links.syntaxLinkName;
  }

  static isSemanticLink(link: dia.CellView | dia.LinkView): boolean {
    return link.model?.attributes?.type === Links.semanticLinkName;
  }

  static targetsSemanticNode(link: dia.LinkView): boolean {
    return link.model?.getTargetElement()?.attributes?.type === Nodes.semanticNodeName;
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
          LinkUtil.layoutSelfLink(graph, cell, sourceId, 0);
        } else {
          cell.unset('vertices');
        }
        break;
      }
      default: {
        // there are multiple siblings
        // we need to create vertices
        if (sourceId === targetId) {
          siblings.forEach((link, index) => LinkUtil.layoutSelfLink(graph, link, sourceId, index));
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
