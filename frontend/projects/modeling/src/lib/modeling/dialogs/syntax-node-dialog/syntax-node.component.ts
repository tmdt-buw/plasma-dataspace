import { Component, ElementRef, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

@Component({
  selector: 'pls-syntax-node-details',
  templateUrl: 'syntax-node.component.html',
  styleUrls: ['../common/dialog.styles.scss']
})
export class PlsSyntaxNodeDetailsComponent implements OnInit {

  private relativeTo: ElementRef;

  constructor(public dialogRef: MatDialogRef<PlsSyntaxNodeDetailsComponent>, @Inject(MAT_DIALOG_DATA) public data: { node: any, relativeTo: ElementRef }) {
    this.relativeTo = data.relativeTo;
  }

  ngOnInit(): void {
    const rect: DOMRect = this.relativeTo.nativeElement.getBoundingClientRect();
    const position = {top: `${rect.top + 15}px`, right: `${rect.left + 15}px`};
    this.dialogRef.updatePosition(position);
  }
}
