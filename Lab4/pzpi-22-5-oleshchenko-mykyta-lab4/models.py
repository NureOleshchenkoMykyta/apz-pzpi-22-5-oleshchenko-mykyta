from sqlalchemy import Column, Integer, String, ForeignKey, DateTime, Text
from sqlalchemy.orm import relationship, declarative_base
from datetime import datetime

Base = declarative_base()

class Account(Base):
    __tablename__ = 'account'
    __table_args__ = {'schema': 'analysisstate'}  # Указываем схему

    account_id = Column('AccountID', Integer, primary_key=True, autoincrement=True)
    email = Column('Email', String(255), unique=True, nullable=False)
    password = Column('Password', String(255), nullable=False)
    name = Column('Name', String(255), nullable=False)

    # Связь с другими таблицами
    results = relationship('Result', back_populates='account', cascade='all, delete-orphan')
    notes = relationship('Note', back_populates='account', cascade='all, delete-orphan')

class Result(Base):
    __tablename__ = 'results'
    __table_args__ = {'schema': 'analysisstate'}  # Указываем схему

    result_id = Column('ResultID', Integer, primary_key=True, autoincrement=True)
    account_id = Column('AccountID', Integer, ForeignKey('analysisstate.account.AccountID', ondelete='CASCADE'))
    analysis_date = Column('AnalysisDate', DateTime, default=datetime.utcnow)
    stress_level = Column('StressLevel', Integer, nullable=False)
    emotional_state = Column('EmotionalState', String(255), nullable=False)

    # Обратная связь
    account = relationship('Account', back_populates='results')

class Note(Base):
    __tablename__ = 'notes'
    __table_args__ = {'schema': 'analysisstate'}  # Указываем схему

    note_id = Column('NoteID', Integer, primary_key=True, autoincrement=True)
    account_id = Column('AccountID', Integer, ForeignKey('analysisstate.account.AccountID', ondelete='CASCADE'))
    creation_date = Column('CreationDate', DateTime, default=datetime.utcnow)
    text = Column('Text', Text, nullable=False)

    # Обратная связь
    account = relationship('Account', back_populates='notes')
