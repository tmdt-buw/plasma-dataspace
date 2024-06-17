import { Component, Input } from '@angular/core';
import { Class, SyntaxNode } from '../../../api/generated/sms';
import { ContextMenuEvent, ContextMenuEventType } from '../../model/events/context-menu-event';
import { ContextMenuService } from '../context-menu.service';

@Component({
  selector: 'pls-entity-concept-menu',
  templateUrl: 'semantic-class-menu.component.html',
  styleUrls: ['../context-menu.less']
})
export class PlsSemanticClassMenuComponent {

  @Input() node: Class;
  @Input() syntaxNode: SyntaxNode;

  constructor(private contextMenu: ContextMenuService) {
  }

  remove(): void {
    this.contextMenu.close(new ContextMenuEvent(ContextMenuEventType.removeEntity, this.node));
  }

  unmap(): void {
    this.node.syntaxNodeUuid = undefined;
    this.node.syntaxLabel = undefined;
    this.node.syntaxPath = undefined;
    this.contextMenu.close(new ContextMenuEvent(ContextMenuEventType.updateEntity, this.node));
  }

  removeInstance(): void {
    this.node.instance = undefined;
    this.contextMenu.close(new ContextMenuEvent(ContextMenuEventType.updateEntity, this.node));
  }
}
