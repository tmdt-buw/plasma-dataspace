import { dia } from 'jointjs';
import { Relation } from '../../../api/generated/dms';
import { Links } from './link';

/**
 * Abstract class that provides configs for papers
 */
export abstract class Papers {

  /**
   * Call constructor for paper
   * @param graph model
   * @param relationConcept function
   * @param primary color function
   */
  static createPaper(graph: dia.Graph, relationConcept: () => Relation, primary: () => string, lockDragAndDrop: boolean): dia.Paper {
    const interactive = () => {
      if (!lockDragAndDrop) {
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
      } } else {
        return {
          labelMove: false,
          addLinkFromMagnet: false,
          elementMove: false
        };
      }
    };
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
      interactive,
      // ony allow links between semantic nodes
      allowLink: (linkView: dia.LinkView) => {
        return Links.targetsSemanticNode(linkView);
      },
      // default link style
      defaultLink: () => {
        const label = relationConcept().label;
        return Links.createDefaultLink(label, primary());
      }
    });
  }
}
