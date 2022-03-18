import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-modeling',
  templateUrl: './modeling.component.html',
  styleUrls: ['./modeling.component.less']
})
export class ModelingComponent implements OnInit {

  modelId: string;

  constructor(private route: ActivatedRoute) { }

  ngOnInit(): void {
    this.modelId = this.route.snapshot.paramMap.get('id');
  }

}
