import { Directive, HostListener } from '@angular/core';
import { MatIcon } from '@angular/material/icon';

@Directive({
  selector: '[hover-accent-icon]'
})
export class HoverAccentDirective {

  constructor(private host: MatIcon) {}

  @HostListener('mouseenter')
  onEnter(): void {
    this.setColor('accent');
  }

  @HostListener('mouseleave')
  onLeave(): void {
    this.setColor('primary');
  }

  private setColor(color: string) {
    // @ts-ignore
    this.host.color = color;
  }


}
