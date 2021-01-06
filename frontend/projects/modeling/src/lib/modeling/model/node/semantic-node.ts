import { dia, shapes } from 'jointjs';
import { Nodes } from './node';

export const semanticNode: dia.Cell.Constructor<dia.Element> = shapes.standard.HeaderedRectangle.define(
  Nodes.semanticNodeName, {
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
