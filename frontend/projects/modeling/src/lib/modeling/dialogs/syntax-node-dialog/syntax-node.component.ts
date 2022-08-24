import { Component, Input } from '@angular/core';
import { PrimitiveNode, SchemaNode } from '../../../api/generated/dms';

@Component({
  selector: 'pls-syntax-node-details',
  templateUrl: 'syntax-node.component.html',
  styleUrls: ['../common/dialog.styles.scss']
})
export class PlsSyntaxNodeDetailsComponent {

  @Input() node: SchemaNode;

  constructor() {
  }

  getExamples(): string[] {
    return this.node?._class === 'PrimitiveNode' ? (this.node as PrimitiveNode).examples : [];
  }

}
