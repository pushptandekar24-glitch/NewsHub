// ============================================
// BACKEND SERVER (server.js)
// ============================================

const express = require('express');
const cors = require('cors');
const axios = require('axios');

const app = express();
const PORT = process.env.PORT || 3001;

// Your NewsAPI Key
const NEWS_API_KEY = '88aaa334b31e468aa2ed839ba7568f9d';
const NEWS_API_BASE = 'https://newsapi.org/v2';

// Middleware
app.use(cors());
app.use(express.json());

// Health check endpoint
app.get('/', (req, res) => {
  res.json({ 
    status: 'Server is running',
    message: 'Global News API Backend',
    timestamp: new Date().toISOString()
  });
});

// Get news by category
app.get('/api/news', async (req, res) => {
  try {
    const { category = 'general', country = 'us', page = 1, pageSize = 30 } = req.query;
    
    const response = await axios.get(`${NEWS_API_BASE}/top-headlines`, {
      params: {
        category,
        country,
        page,
        pageSize,
        apiKey: NEWS_API_KEY
      }
    });

    res.json(response.data);
  } catch (error) {
    console.error('Error fetching news:', error.response?.data || error.message);
    res.status(500).json({ 
      error: 'Failed to fetch news',
      message: error.response?.data?.message || error.message
    });
  }
});

// Search news by keyword
app.get('/api/search', async (req, res) => {
  try {
    const { q, page = 1, pageSize = 30, sortBy = 'publishedAt' } = req.query;
    
    if (!q) {
      return res.status(400).json({ error: 'Search query is required' });
    }

    const response = await axios.get(`${NEWS_API_BASE}/everything`, {
      params: {
        q,
        page,
        pageSize,
        sortBy,
        apiKey: NEWS_API_KEY
      }
    });

    res.json(response.data);
  } catch (error) {
    console.error('Error searching news:', error.response?.data || error.message);
    res.status(500).json({ 
      error: 'Failed to search news',
      message: error.response?.data?.message || error.message
    });
  }
});

// Get news from specific sources
app.get('/api/sources', async (req, res) => {
  try {
    const { category, language = 'en', country } = req.query;
    
    const response = await axios.get(`${NEWS_API_BASE}/sources`, {
      params: {
        category,
        language,
        country,
        apiKey: NEWS_API_KEY
      }
    });

    res.json(response.data);
  } catch (error) {
    console.error('Error fetching sources:', error.response?.data || error.message);
    res.status(500).json({ 
      error: 'Failed to fetch sources',
      message: error.response?.data?.message || error.message
    });
  }
});

// Get news by specific source
app.get('/api/news-by-source', async (req, res) => {
  try {
    const { sources, page = 1, pageSize = 30 } = req.query;
    
    if (!sources) {
      return res.status(400).json({ error: 'Sources parameter is required' });
    }

    const response = await axios.get(`${NEWS_API_BASE}/top-headlines`, {
      params: {
        sources,
        page,
        pageSize,
        apiKey: NEWS_API_KEY
      }
    });

    res.json(response.data);
  } catch (error) {
    console.error('Error fetching news by source:', error.response?.data || error.message);
    res.status(500).json({ 
      error: 'Failed to fetch news by source',
      message: error.response?.data?.message || error.message
    });
  }
});

// Error handling middleware
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({ error: 'Something went wrong!' });
});

// Start server
app.listen(PORT, () => {
  console.log(`✅ Server is running on http://localhost:${PORT}`);
  console.log(`📰 News API Backend ready!`);
});


// ============================================
// PACKAGE.JSON
// ============================================
/*
{
  "name": "global-news-backend",
  "version": "1.0.0",
  "description": "Backend server for Global News application",
  "main": "server.js",
  "scripts": {
    "start": "node server.js",
    "dev": "nodemon server.js"
  },
  "dependencies": {
    "express": "^4.18.2",
    "cors": "^2.8.5",
    "axios": "^1.6.0"
  },
  "devDependencies": {
    "nodemon": "^3.0.1"
  }
}
*/