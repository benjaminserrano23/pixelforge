import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import GameCard from './GameCard'
import type { Game } from '../types'

const baseGame: Game = {
  id: 1,
  title: 'Cave Story',
  description: 'Metroidvania',
  genre: 'Platformer',
  price: 9.99,
  coverImageUrl: null,
  status: 'PUBLISHED',
  developerId: 1,
  createdAt: '',
  updatedAt: '',
}

function renderCard(game: Game, showStatus = false) {
  return render(
    <MemoryRouter>
      <GameCard game={game} to={`/games/${game.id}`} showStatus={showStatus} />
    </MemoryRouter>,
  )
}

describe('GameCard', () => {
  it('muestra título, género y precio formateado con 2 decimales', () => {
    renderCard({ ...baseGame, price: 9.9 })

    expect(screen.getByText('Cave Story')).toBeInTheDocument()
    expect(screen.getByText('Platformer')).toBeInTheDocument()
    expect(screen.getByText('$9.90')).toBeInTheDocument()
  })

  it('muestra "Sin portada" cuando el juego no tiene coverImageUrl', () => {
    renderCard(baseGame)
    expect(screen.getByText('Sin portada')).toBeInTheDocument()
  })

  it('renderiza la imagen cuando hay coverImageUrl', () => {
    renderCard({ ...baseGame, coverImageUrl: '/uploads/cover.png' })
    expect(screen.getByRole('img', { name: 'Cave Story' })).toHaveAttribute('src', '/uploads/cover.png')
  })

  it('no muestra el badge de status salvo que showStatus sea true', () => {
    renderCard(baseGame, false)
    expect(screen.queryByText('Publicado')).not.toBeInTheDocument()
  })

  it('muestra "Borrador" para juegos DRAFT cuando showStatus es true', () => {
    renderCard({ ...baseGame, status: 'DRAFT' }, true)
    expect(screen.getByText('Borrador')).toBeInTheDocument()
  })

  it('muestra "Publicado" para juegos PUBLISHED cuando showStatus es true', () => {
    renderCard({ ...baseGame, status: 'PUBLISHED' }, true)
    expect(screen.getByText('Publicado')).toBeInTheDocument()
  })
})
