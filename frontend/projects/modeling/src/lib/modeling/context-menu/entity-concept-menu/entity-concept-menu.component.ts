import { Component, Input } from '@angular/core';
import { EntityType, SyntacticOperationDTO } from '../../../api/generated/dms';
import { ContextMenuEvent, ContextMenuEventType } from '../../model/events/context-menu-event';
import { ContextMenuService } from '../context-menu.service';
import { SyntaxNode } from '../../model/configuration/node';

@Component({
  selector: 'pls-entity-concept-menu',
  templateUrl: 'entity-concept-menu.component.html',
  styleUrls: ['../context-menu.scss']
})
export class PlsEntityConceptMenuComponent {

  @Input() entity: EntityType;
  @Input() syntaxNode: SyntaxNode;

  constructor(private contextMenu: ContextMenuService) {
  }

  onSelectOperation(operation: SyntacticOperationDTO): void {
    this.contextMenu.close(new ContextMenuEvent(ContextMenuEventType.performOperation, this.entity, operation));
  }

  remove(): void {
    this.contextMenu.close(new ContextMenuEvent(ContextMenuEventType.removeEntity, this.entity));
  }
}
