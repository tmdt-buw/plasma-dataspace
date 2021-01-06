import { dia, shapes } from 'jointjs';
import { CollisionSchemaDTO, CompositeDTO, ObjectNodeDTO, PrimitiveDTO, SchemaNodeDTO, SetNodeDTO } from '../../../api/generated/dms';
import { Nodes } from './node';

export type SyntaxNode = SchemaNodeDTO | CollisionSchemaDTO | CompositeDTO | ObjectNodeDTO | PrimitiveDTO | SetNodeDTO;

export const syntaxNode: dia.Cell.Constructor<dia.Element> = shapes.standard.Rectangle.define(
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
      selector: 'body',
    }, {
      tagName: 'text',
      selector: 'icon'
    }, {
      tagName: 'text',
      selector: 'label'
    }]
  }
);
