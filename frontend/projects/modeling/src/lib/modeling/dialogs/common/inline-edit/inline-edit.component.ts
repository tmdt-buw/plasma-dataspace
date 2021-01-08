import { Component, ElementRef, forwardRef, Input, Renderer2 } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

@Component({
  selector: 'pls-inline-edit',
  templateUrl: './inline-edit.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => InlineInputEditComponent),
      multi: true
    }
  ]
})
export class InlineInputEditComponent implements ControlValueAccessor {

  // The control label
  @Input() label = '';
  // Type of input control
  @Input() type = 'text';
  // Input value required
  @Input() required = false;
  // Input control is disabled
  @Input() disabled = false;
  // Prefix the value
  @Input() prefix: any;
  // color of the confirm button
  @Input() confirmColor = 'primary';
  // color of the cancel button
  @Input() cancelColor = 'warn';
  @Input() editing = false;
  @Input() empty = '';

  // private value of input
  private _value = '';
  // value prior to editing
  private preValue = '';
  // Callback when the value is changing
  public onChange: any = Function.prototype;
  // Callback when the input is accessed
  public onTouched: any = Function.prototype;

  get value(): any {
    return this._value;
  }

  set value(v: any) {
    if (v !== this._value) {
      this._value = v;
      this.onChange(v);
    }
  }

  // ControlValueAccessor interface impl
  writeValue(value: any): void {
    if (value !== undefined) {
      this._value = value;
    }
  }

  // ControlValueAccessor interface impl
  public registerOnChange(fn: (_: any) => {}): void {
    this.onChange = fn;
  }

  // ControlValueAccessor interface impl
  public registerOnTouched(fn: () => {}): void {
    this.onTouched = fn;
  }

  cancel(): void {
    this.value = this.preValue;
    this.editing = false;
  }

  confirm(): void {
    this.editing = false;
  }

  keypress($event): void {
    if ($event.keyCode === 13) {
      this.confirm();
    }
  }

  // Start editing
  edit(value): void {
    if (this.disabled) {
      return;
    }
    this.preValue = value;
    this.editing = true;
  }

  constructor(element: ElementRef, private _renderer: Renderer2) {
  }

}
