import {
  Directive,
  ElementRef,
  EventEmitter,
  HostBinding,
  HostListener,
  Input,
  Output,
  Renderer2
} from '@angular/core';

@Directive({
  selector: '[plsFileDrop]'
})
export class PlsFileDropDirective {
  @Input() multiple: boolean = false;
  @Input() disabled: boolean = false;

  @Output() fileDrop: EventEmitter<FileList | File> = new EventEmitter<FileList | File>();

  /**
   * Binds native 'multiple' attribute if [multiple] property is 'true'.
   */
  @HostBinding('attr.multiple')
  get multipleBinding(): string {
    return this.multiple ? '' : undefined;
  }

  /**
   * Binds native 'disabled' attribute if [disabled] property is 'true'.
   */
  @HostBinding('attr.disabled')
  get disabledBinding(): string {
    return this.disabled ? '' : undefined;
  }

  constructor(private _renderer: Renderer2, private _element: ElementRef) {
  }

  @HostListener('drop', ['$event'])
  onDrop(event: DragEvent): void {
    if (!this.disabled) {
      const transfer: DataTransfer = event.dataTransfer;
      const files: FileList = transfer.files;
      if (files.length) {
        const value: FileList | File = this.multiple ? (files.length > 1 ? files : files[0]) : files[0];
        this.fileDrop.emit(value);
      }
    }
    this._renderer.removeClass(this._element.nativeElement, 'drop-zone-dragged');
    this.preventAndStop(event);
  }

  @HostListener('dragover', ['$event'])
  onDragOver(event: DragEvent): void {
    const transfer: DataTransfer = event.dataTransfer;
    transfer.dropEffect = this.typeCheck(transfer.types);
    if (
      this.disabled ||
      (!this.multiple && ((transfer.items && transfer.items.length > 1) || (<any>transfer).mozItemCount > 1))
    ) {
      transfer.dropEffect = 'none';
    } else {
      transfer.dropEffect = 'copy';
    }
    this.preventAndStop(event);
  }

  @HostListener('dragenter', ['$event'])
  onDragEnter(event: DragEvent): void {
    if (!this.disabled) {
      this._renderer.addClass(this._element.nativeElement, 'drop-zone-dragged');
    }
    this.preventAndStop(event);
  }

  @HostListener('dragleave', ['$event'])
  onDragLeave(event: DragEvent): void {
    // @ts-ignore
    const el = event.fromElement;
    if (el.contains(this._element.nativeElement)) {
      this._renderer.removeClass(this._element.nativeElement, 'drop-zone-dragged');
      this.preventAndStop(event);
    }
  }

  private typeCheck(types: ReadonlyArray<string> | DOMStringList): string {
    let dropEffect: string = 'none';
    if (
      types &&
      (((<any>types).contains && (<any>types).contains('Files')) ||
        ((<any>types).indexOf && (<any>types).indexOf('Files') !== -1))
    ) {
      dropEffect = 'copy';
    }

    return dropEffect;
  }

  private preventAndStop(event: Event): void {
    event.stopPropagation();
    event.preventDefault();
  }
}
