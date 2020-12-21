import { Component, Input } from '@angular/core';
import { EntityConceptDTO } from '../../../api/generated/dms';
import { ContextMenuEvent, ContextMenuEventType } from '../context-menu-event';
import { ContextMenuService } from '../context-menu.service';

@Component({
  selector: 'pls-entity-concept-menu',
  templateUrl: 'entity-concept-menu.component.html',
  styleUrls: ['../context-menu.scss']
})
export class PlsEntityConceptMenuComponent {

  @Input() concept: EntityConceptDTO;

  constructor(private contextMenu: ContextMenuService) {
  }

  remove(): void {
    this.contextMenu.close(new ContextMenuEvent(ContextMenuEventType.removeEntity, this.concept));
  }
}
