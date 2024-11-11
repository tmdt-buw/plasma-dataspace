import { Component, Input, OnInit } from '@angular/core';
import {
  PreferredServices,
  ArsRegistrationControllerService,
  RecommendationServiceRegistration,
  ModelingControllerService
} from '../../../api/generated/sms';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'pls-ars-selection-dialog',
  templateUrl: './ars-selection-dialog.component.html',
  styleUrls: ['./ars-selection-dialog.component.less']
})
export class ArsSelectionDialogComponent implements OnInit {

  constructor(
    private arsServices: ArsRegistrationControllerService,
    private modelingService: ModelingControllerService
  ) { }

  @Input() modelId: string;

  serviceList: RecommendationServiceRegistration[];
  preferredServices: PreferredServices;

  selected: RecommendationServiceRegistration;

  // init, innit
  ngOnInit(): void {
    forkJoin([
      this.arsServices.aRSList(),
      this.modelingService.getPreferredServices(this.modelId)
    ]).subscribe(([services, preferredServices]) => {
      this.serviceList = services;
      this.preferredServices = preferredServices;
      this.filterActiveServices();
    });
  }

  filterActiveServices(): void {
    for (const category of Object.keys(this.preferredServices)) {
      this.preferredServices[category].forEach((uuid: string) => {
        if (!this.serviceIsActive(uuid)) {
          this.removeServiceFromPreferred(uuid);
        }
      });
    }
  }

  serviceIsActive(uuid: string): boolean {
    return this.serviceList.some((service) => service.uuid === uuid);
  }

  serviceIsPreferred(uuid: string): boolean {
    if (this.preferredServices === undefined) { return false; }
    return this.preferredServices.preferredLabeling.includes(uuid) ||
      this.preferredServices.preferredModeling.includes(uuid) ||
      this.preferredServices.preferredRefinement.includes(uuid) ;
  }

  addServiceToPreferred(service: RecommendationServiceRegistration): void {
    switch (service.recommendationType) {
      case 'Labeling':
        this.preferredServices.preferredLabeling.push(service.uuid);
        break;
      case 'Modeling':
        this.preferredServices.preferredModeling.push(service.uuid);
        break;
      case 'Refinement':
        this.preferredServices.preferredRefinement.push(service.uuid);
        break;
    }
    this.modelingService.updatePreferredServices(this.modelId, this.preferredServices).subscribe();
  }

  removeServiceFromPreferred(uuid: string): void {
    this.preferredServices.preferredLabeling = this.preferredServices.preferredLabeling.filter(item => item !== uuid);
    this.preferredServices.preferredModeling = this.preferredServices.preferredModeling.filter(item => item !== uuid);
    this.preferredServices.preferredRefinement = this.preferredServices.preferredRefinement.filter(item => item !== uuid);
    this.modelingService.updatePreferredServices(this.modelId, this.preferredServices).subscribe();
  }

  selectService(service: RecommendationServiceRegistration): void {
    this.selected = service;
  }

  isSelected(service: RecommendationServiceRegistration): boolean {
    return this.selected?.uuid === service.uuid;
  }
}
