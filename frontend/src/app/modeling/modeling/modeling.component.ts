import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ModelingControllerService, ModelingInfo } from '@tmdt/dms';

@Component({
  selector: 'app-modeling',
  templateUrl: './modeling.component.html',
  styleUrls: ['./modeling.component.less']
})
export class ModelingComponent implements OnInit {

  modelId: string;
  model: ModelingInfo;

  constructor(private route: ActivatedRoute, private modelingService: ModelingControllerService) {
  }

  ngOnInit(): void {
    this.modelId = this.route.snapshot.paramMap.get('id');
    this.modelingService.listModelings().subscribe(modelings => this.model = modelings.find(model => model.id === this.modelId));
  }

}
