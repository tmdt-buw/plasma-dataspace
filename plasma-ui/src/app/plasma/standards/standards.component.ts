import { Component, OnInit } from '@angular/core';
import {EntityRuleDTO, RelationRuleDTO, RuleProvisionDTO} from '../../../../build/openapi';
import {AnalysisControllerService} from '../../../../build/openapi/api/analysisController.service';

@Component({
  selector: 'app-standards',
  templateUrl: './standards.component.html',
  styleUrls: ['./standards.component.scss']
})
export class StandardsComponent implements OnInit {

  ruleProvision: RuleProvisionDTO;

  public standardName: string;
  public standardDescription: string;

  public entityConcepts: EntityRuleDTO[] = [
    {originalAttribute: '', conceptName: '', conceptDescription: ''}
  ];
  public relationConcepts: RelationRuleDTO[] = [
    {conceptName: '', fromConcept: '', toConcept: '', conceptDescription: ''}
  ];

  constructor(private readonly schemaanalysisController: AnalysisControllerService) { }

  ngOnInit() {
  }

  public addEntityConcept() {
    this.entityConcepts.push({originalAttribute: '', conceptName: '', conceptDescription: ''});
  }

  public deleteEntityConcept(index: number) {
    this.entityConcepts.splice(index, 1);
    if (this.entityConcepts.length === 0) {
      this.entityConcepts = [{originalAttribute: '', conceptName: '', conceptDescription: ''}];
    }
  }

  public addRelationConcept() {
    this.relationConcepts.push({conceptName: '', fromConcept: '', toConcept: '', conceptDescription: ''});
  }

  public deleteRelationConcept(index: number) {
    this.relationConcepts.splice(index, 1);
    if (this.relationConcepts.length === 0) {
      this.relationConcepts = [{conceptName: '', fromConcept: '', toConcept: '', conceptDescription: ''}];
    }
  }

  public cleanRules() {
    if (this.entityConcepts[this.entityConcepts.length - 1].conceptName === '') {
      this.entityConcepts.pop();
    }
    if (this.relationConcepts[this.relationConcepts.length - 1].conceptName === '') {
      this.relationConcepts.pop();
    }
  }

  public async saveStandard() {
    this.cleanRules();
    const ruleProvisionDTO: RuleProvisionDTO = {
      name: this.standardName,
      description: this.standardDescription,
      entityRules: this.entityConcepts,
      relationRules: this.relationConcepts
    };
    await this.schemaanalysisController.createStandard(ruleProvisionDTO).toPromise();
  }

  public onFileChanged(event) {
    const reader = new FileReader();
    reader.readAsText(event.target.files[0], 'UTF-8');
    reader.onload = () => {
      if (typeof reader.result === 'string') {
        const jsonRule = JSON.parse(reader.result);
        this.ruleProvision = jsonRule as RuleProvisionDTO;
        this.entityConcepts = this.ruleProvision.entityRules;
        this.relationConcepts = this.ruleProvision.relationRules;
        this.standardName = this.ruleProvision.name;
        this.standardDescription = this.ruleProvision.description;
      }
    };
    reader.onerror = (error) => {
      console.log(error);
    };
  }

}
