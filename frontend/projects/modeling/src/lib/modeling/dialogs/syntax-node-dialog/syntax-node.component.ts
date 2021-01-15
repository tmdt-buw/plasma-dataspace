import { Component, ElementRef, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export interface PlsSyntaxNodeDetailsData {
  node: any;
  relativeTo: ElementRef;
}

@Component({
  selector: 'pls-syntax-node-details',
  templateUrl: 'syntax-node.component.html',
  styleUrls: ['../common/dialog.styles.scss']
})
export class PlsSyntaxNodeDetailsComponent implements OnInit {

  private relativeTo: ElementRef;

  constructor(public dialogRef: MatDialogRef<PlsSyntaxNodeDetailsComponent>, @Inject(MAT_DIALOG_DATA) public data: PlsSyntaxNodeDetailsData) {
    this.relativeTo = data.relativeTo;
  }

  ngOnInit(): void {
    const rect: DOMRect = this.relativeTo.nativeElement.getBoundingClientRect();
    const position = {top: `${rect.top + 15}px`, right: `${rect.left + 15}px`};
    this.dialogRef.updatePosition(position);
  }
}
