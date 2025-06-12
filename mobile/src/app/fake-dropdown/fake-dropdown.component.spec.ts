import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { IonicModule } from '@ionic/angular';

import { FakeDropdownComponent } from './fake-dropdown.component';

describe('FakeDropdownComponent', () => {
  let component: FakeDropdownComponent;
  let fixture: ComponentFixture<FakeDropdownComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
    imports: [IonicModule.forRoot(), FakeDropdownComponent]
}).compileComponents();

    fixture = TestBed.createComponent(FakeDropdownComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
