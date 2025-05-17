import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { AskForPhoneComponent } from './ask-for-phone.component';

describe('AskForPhoneComponent', () => {
  let component: AskForPhoneComponent;
  let fixture: ComponentFixture<AskForPhoneComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [AskForPhoneComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(AskForPhoneComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
