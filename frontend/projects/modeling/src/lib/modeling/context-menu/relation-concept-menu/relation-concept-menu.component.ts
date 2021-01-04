import { Component, Input } from '@angular/core';
import { RelationConceptDTO } from '../../../api/generated/dms';
import { ContextMenuService } from '../context-menu.service';
import { ContextMenuEvent, ContextMenuEventType } from '../context-menu-event';

@Component({
  selector: 'pls-relation-concept-menu',
  templateUrl: 'relation-concept-menu.component.html',
  styleUrls: ['../context-menu.scss']
})
export class PlsRelationConceptMenuComponent {

  @Input() concept: RelationConceptDTO;

  constructor(private contextMenu: ContextMenuService) {
  }

  remove(): void {
    this.contextMenu.close(new ContextMenuEvent(ContextMenuEventType.removeRelation, this.concept));
  }
}
