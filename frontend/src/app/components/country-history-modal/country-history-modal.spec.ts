import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CountryHistoryModalComponent } from './country-history-modal';

describe('CountryHistoryModalComponent', () => {
  let component: CountryHistoryModalComponent;
  let fixture: ComponentFixture<CountryHistoryModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CountryHistoryModalComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CountryHistoryModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
