import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-modeling',
  templateUrl: './modeling.component.html',
  styleUrls: ['./modeling.component.scss']
})
export class ModelingComponent implements OnInit {

  dataSource: string;

  constructor(private route: ActivatedRoute) { }

  ngOnInit(): void {
    this.dataSource = this.route.snapshot.paramMap.get('id');
  }

}
