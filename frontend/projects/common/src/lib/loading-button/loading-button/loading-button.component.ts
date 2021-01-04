import { Component, Input, OnInit } from '@angular/core';

@Component({
  selector: 'pls-loading-button',
  templateUrl: './loading-button.component.html',
  styleUrls: ['./loading-button.component.scss']
})
export class PlsLoadingButtonComponent implements OnInit {

  @Input() color;
  @Input() disabled;
  @Input() loading;
  @Input() icon;
  @Input() label;

  diameter = 20;

  constructor() {
  }

  ngOnInit(): void {
  }

}
