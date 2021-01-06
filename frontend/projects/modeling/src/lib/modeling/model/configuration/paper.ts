import { dia, shapes } from 'jointjs';
import { LinkUtil } from '../link/link-util';
import { RelationConceptDTO } from '../../../api/generated/dms';

export function plasmaPaper(graph: dia.Graph, relationConcept: () => RelationConceptDTO): dia.Paper {
  return new dia.Paper({
    model: graph,
    width: '100%',
    height: '100%',
    background: {
      opacity: 0
    },
    gridSize: 1,
    frozen: true,
    async: true,
    // disable links pinning to blank space since we only want to connect nodes with links
    linkPinning: false,
    // enable link drawing and disable move nodes if relation is selected in menu
    interactive: () => {
      if (relationConcept()) {
        return {
          labelMove: true,
          addLinkFromMagnet: true,
          elementMove: false
        };
      } else {
        return {
          labelMove: true,
          addLinkFromMagnet: false,
          elementMove: true
        };
      }
    },
    // ony allow links between semantic nodes
    allowLink: (linkView: dia.LinkView) => {
      return LinkUtil.targetsSemanticNode(linkView);
    },
    // mark allowed targets (jointjs adds class "available-magnet")
    markAvailable: true,
    defaultLink: () => {
      const text = relationConcept().name;
      const labels = [{
        attrs: {
          text: {
            text,
            fill: this.colorToHex('primary')
          }
        },
        position: {
          distance: 0.5,
          offset: (text.indexOf('\n') > -1 || text.length === 1) ? 0 : 10,
          args: {
            ensureLegibility: true
          }
        }
      }];
      return new shapes.standard.Link(
        {
          labels,
          attrs: {
            line: {
              stroke: this.colorToHex('primary'),
              strokeOpacity: 1,
              strokeWidth: 1
            }
          }
        }
      );
    }
  });
}
